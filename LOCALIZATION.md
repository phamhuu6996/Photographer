# Hướng dẫn Localization (Đa ngôn ngữ)

## Cách hoạt động

Android tự động chọn ngôn ngữ dựa trên **cài đặt ngôn ngữ hệ thống** của thiết bị:

### 1. Cấu trúc thư mục

```
app/src/main/res/
├── values/              # Tiếng Việt (default)
│   └── strings.xml
└── values-en/           # Tiếng Anh
    └── strings.xml
```

### 2. Cách Android chọn ngôn ngữ

- **Khi hệ thống là tiếng Anh** → Android tự động load `values-en/strings.xml`
- **Khi hệ thống là tiếng Việt hoặc ngôn ngữ khác** → Android load `values/strings.xml` (default)

### 3. Không cần code thêm

Android tự động xử lý việc chọn ngôn ngữ. Khi bạn dùng:
```kotlin
stringResource(R.string.gallery)
```

Android sẽ tự động:
- Tìm trong `values-en/strings.xml` nếu hệ thống là tiếng Anh
- Tìm trong `values/strings.xml` nếu hệ thống là tiếng Việt hoặc ngôn ngữ khác

## Thêm ngôn ngữ mới

Để thêm ngôn ngữ mới (ví dụ: tiếng Pháp):

1. Tạo thư mục: `app/src/main/res/values-fr/`
2. Tạo file: `strings.xml` với nội dung tiếng Pháp
3. Android sẽ tự động sử dụng khi hệ thống là tiếng Pháp

## Danh sách mã ngôn ngữ phổ biến

- `values-en/` - Tiếng Anh (English)
- `values-vi/` - Tiếng Việt (Vietnamese) - có thể tạo nếu muốn explicit
- `values-fr/` - Tiếng Pháp (French)
- `values-de/` - Tiếng Đức (German)
- `values-ja/` - Tiếng Nhật (Japanese)
- `values-ko/` - Tiếng Hàn (Korean)
- `values-zh/` - Tiếng Trung (Chinese)

## Lưu ý

- File `values/strings.xml` là **default** - sẽ được dùng nếu không tìm thấy ngôn ngữ phù hợp
- Tất cả các string trong code phải dùng `stringResource()` thay vì hardcode
- Khi thêm string mới, nhớ thêm vào **tất cả** các file strings.xml

