## Tài liệu Fastlane & CI

Tài liệu này giải thích chi tiết các file cốt lõi giúp dự án build bằng Fastlane và GitHub Actions. Với mỗi file sẽ có:
- **Why (Tại sao):** lý do tồn tại
- **What (Chứa gì):** nội dung/chức năng
- **When (Khi nào dùng):** thời điểm Fastlane/CI cần tới
- **How (Cách dùng):** thao tác chỉnh sửa/mở rộng

---

### `Gemfile`
- **Why:** Đảm bảo tất cả mọi người (và CI) cài đúng version Ruby gem, tránh lệch môi trường.
- **What:** Khai báo `gem "fastlane", "2.229.0"` để Bundler luôn kéo Fastlane 2.229.0; tự động load `fastlane/Pluginfile` nếu có plugin.
- **When:**  
  - Chạy `bundle install` lần đầu trên máy mới hoặc runner CI.  
  - Khi thực thi Fastlane bằng `bundle exec fastlane ...` (khuyến nghị) để dùng đúng gems đã khai báo.  
- **How:**  
  - Muốn nâng/giảm version Fastlane thì chỉnh dòng gem tương ứng.  
  - Thêm gem khác (ví dụ plugin) rồi chạy lại `bundle install`.  
  - Commit `Gemfile` (và `Gemfile.lock` nếu sinh ra) để chia sẻ cho team.

---

### `fastlane/Appfile`
- **Why:** Lưu thông tin định danh app, giúp mọi lane biết cần build/deploy package nào.
- **What:** Đặt `app_identifier("com.phamhuu.photographer")`, `package_name("com.phamhuu.photographer")` và tự đọc credentials nếu có biến môi trường:
  - `PLAY_JSON_KEY_PATH`: đường dẫn file JSON service account (nằm ngoài repo).
  - `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_KEY`: nội dung JSON (ví dụ set qua GitHub Secrets).
- **When:** Fastlane tự load file này trước mỗi lane, bạn không phải thao tác gì trừ khi đổi package hoặc cung cấp credentials.
- **How:**  
  - Đổi `app_identifier`/`package_name` nếu package app thay đổi.  
  - Local: đặt `export PLAY_JSON_KEY_PATH=/abs/path/service_account.json` (file JSON nằm ngoài repo) rồi chạy lane deploy.  
  - CI: thêm secret `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_KEY` trong GitHub → Settings → Secrets; workflow sẽ tự đọc.  
  - `PLAY_JSON_KEY_PATH` và `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_KEY` đều trỏ tới **cùng một service account JSON** (một cái bằng đường dẫn, một cái bằng nội dung). Chỉ cần set **một** trong hai biến.  
  - Không bao giờ commit JSON key vào repo.  
  - Dòng `team_id` trong file chỉ dành cho iOS/App Store Connect, Android không cần điền.

#### Cách export biến môi trường (ví dụ)
```bash
# Local (dùng file JSON)
export PLAY_JSON_KEY_PATH=/Users/ban/.secrets/play-service-account.json
bundle exec fastlane android beta

# CI (GitHub Actions)
#   Settings → Secrets → Actions → New repository secret
#   Name: GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_KEY
#   Value: copy toàn bộ nội dung file JSON
```

---

### `fastlane/Fastfile`
- **Why:** Nơi định nghĩa toàn bộ “lane” (build/test/lint/deploy) để tự động hóa.
- **What:** Các lane chính cho Android:  
  - `build_bundle`, `build_release`, `screenshots`  
  - `test`, `lint`  
  - `increment_version_code`, `increment_version_name`  
  - `beta` (build bundle + upload Internal Testing), `release` (Production)  
- **When:** Chạy `bundle exec fastlane android <lane>` hoặc khi CI gọi đến.
- **How:**  
  - Muốn thay đổi logic lane thì sửa trực tiếp block tương ứng.  
  - Dùng biến môi trường cho secret.  
  - Với lane deploy, cần có credentials Google Play (`json_key_file` local hoặc secret `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_KEY` trên CI).  
  - Nên chạy lane thử local trước khi đưa vào CI.

---

### `fastlane/Pluginfile`
- **Why:** Tập trung quản lý version plugin Fastlane (nếu có dùng).
- **What:** Hiện chỉ có header mặc định, chưa khai báo plugin nào.
- **When:** Chưa được dùng cho tới khi bổ sung plugin; Bundler vẫn kiểm tra file này qua `Gemfile`.
- **How:**  
  - Thêm plugin bằng `gem "fastlane-plugin-..."` rồi chạy `bundle install`.  
  - Gọi action plugin trong `Fastfile`.  
  - Xóa dòng tương ứng nếu muốn gỡ plugin.

---

### `.github/workflows/ci.yml`
- **Why:** Tự động hóa build/test (và deploy Internal Testing) trên GitHub Actions khi code lên `main`.
- **What:** Workflow “CI/CD Pipeline” gồm:  
  1. Trigger khi push hoặc pull request vào `main` (và có thể chạy tay qua `workflow_dispatch`).  
  2. Runner `ubuntu-latest`.  
  3. Cài JDK 17, Ruby 3.2, Bundler, cache Gradle.  
  4. Chạy các lane Fastlane: `test`, `lint`, `build_bundle`.  
  5. Upload artifact AAB duy nhất (`app-release-aab`).  
  6. Nếu là push lên `main`, tự động chạy `fastlane android beta` (Internal Testing) và `fastlane android release` (Production) với secret `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_KEY`.  
- **When:** Tự chạy khi push/PR vào `main`, hoặc khi bạn chọn run trong tab Actions.
- **How:**  
  - Theo dõi tiến trình ở tab Actions, click run để xem log.  
  - Muốn chỉnh bước (ví dụ bỏ lint) thì edit YAML.  
  - Thêm secrets/biến môi trường trong repo settings.  
  - Bổ sung job nếu cần (test UI, static analysis...).  
  - Tải APK/AAB từ mục Artifacts của run.

---

### Chuẩn bị upload lên Google Play
1. **Service account & quyền**  
   - Tạo service account trong Google Cloud, bật Google Play Android Developer API.  
   - Liên kết service account trong Play Console → API access → cấp quyền “Release manager” (hoặc cao hơn).  
   - Tải file JSON và cung cấp cho Fastlane qua `PLAY_JSON_KEY_PATH` (đường dẫn file) hoặc `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_KEY` (nội dung JSON), chỉ cần một trong hai.
2. **App đã tồn tại trên Play Console**  
   - Tạo ứng dụng với package `com.phamhuu.photographer`.  
   - Điền đủ thông tin bắt buộc (icon, mô tả, chính sách bảo mật, phân loại, rating...).  
3. **Keystore & build hợp lệ**  
   - `app/build.gradle.kts` cần có `signingConfigs.release` trùng keystore đăng ký trên Play Console.  
   - Các lane `build_bundle` / `build_release` tạo AAB/APK đã ký bằng key đó.  
   - Lane `screenshots` cần có instrumentation tests gọi `Screengrab.screenshot("name")`; kết quả lưu tại `fastlane/metadata/android/<locale>/images`.  
4. **Secrets trên CI**  
   - Muốn workflow auto upload → thêm secret `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_KEY` trong GitHub.  
   - Có thể bổ sung secret khác (ví dụ track tùy chỉnh) nếu mở rộng lane.

---

### Quy trình điển hình
#### Flow chạy local
> Có thể chạy toàn bộ chuỗi lệnh bằng `./scripts/local_fastlane_flow.sh` (kịch bản đã chạy test → lint → build bundle).

1. Khởi tạo: `bundle install`, thiết lập `PLAY_JSON_KEY_PATH` nếu cần deploy.  
2. Build thử: `bundle exec fastlane android build_bundle`.  
3. Test & lint: `bundle exec fastlane android test` và `lint`.  
4. Chụp screenshot (nếu có test UI): `bundle exec fastlane android screenshots`.  
5. Deploy nội bộ: `bundle exec fastlane android beta`.  
6. Release production: `bundle exec fastlane android release`.  
> Script `./scripts/local_fastlane_flow.sh` tự export `PLAY_JSON_KEY_PATH=/path/to/your/service_account.json`. Nhớ chỉnh lại đường dẫn thực tế trước khi chạy.

#### Flow chạy CI trên GitHub
1. Push/PR vào `main` sẽ kích hoạt workflow `.github/workflows/ci.yml`.  
2. Runner thiết lập JDK, Ruby, Fastlane, cache Gradle, rồi chạy lần lượt:
   - `fastlane android test`
   - `fastlane android lint`
   - `fastlane android build_bundle`
   - Upload AAB artifact
   - Nếu là push lên `main`: chạy thêm `fastlane android beta` và `fastlane android release` (cần secret `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_KEY`).  
3. Xem kết quả tại tab Actions (log + artifact). Có thể “Re-run jobs” nếu cần.

---

### Chạy Fastlane trong một terminal
1. Mở terminal mới (hoặc tab mới).  
2. Export biến môi trường nếu cần deploy:
   ```bash
   export PLAY_JSON_KEY_PATH=/Users/ban/.secrets/play.json
   # hoặc: export GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_KEY='{"type":"service_account",...}'
   ```
3. CD vào thư mục dự án:
   ```bash
   cd /Users/phamhuu/Documents/out_project/Photographer
   ```
4. Chạy lệnh mong muốn:
   ```bash
   # Build
   bundle exec fastlane android build_debug

   # Upload Internal Testing
   bundle exec fastlane android beta
   ```
5. Nếu cần chạy lane khác thì dùng cùng terminal đó (biến môi trường vẫn còn hiệu lực). Khi đóng terminal, biến sẽ mất.

Bộ file nhỏ gọn này giúp mọi môi trường (dev và CI) dùng chung pipeline Fastlane ổn định.

