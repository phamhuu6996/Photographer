import android.content.Context
import com.google.android.filament.utils.Float3
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.phamhuu.photographer.enums.TypeModel3D
import com.phamhuu.photographer.models.ar.FilamentModelManager
import com.phamhuu.photographer.models.ar.ModelInstance

class Manager3DHelper(
    val filamentHelper: FilamentHelper,
) {
    private val glassModel: FilamentModelManager = FilamentModelManager(
        modelUrl = "models/glasses.glb",
    )

    private val hatModel: FilamentModelManager = FilamentModelManager(
        modelUrl = "models/gun.glb",
    )

    fun selectModel3D(
        context: Context,
        typeModel3D: TypeModel3D
    ) {
        filamentHelper.removeEntities()
        var model: FilamentModelManager? = null
        when (typeModel3D) {
            TypeModel3D.GLASSES -> {
                model = glassModel
            }
            TypeModel3D.HAT -> {
                // Add other models here if needed
                model = hatModel
            }
        }
        val result = filamentHelper.addModel3D(
            model.modelUrl,
            context,
        )

        model.modelInstance = result
        filamentHelper.addModel(model)
        filamentHelper.setVisibility(model, false)
    }

    private fun updateModel(
        model: FilamentModelManager,
        first: Float3? = null,
        second: Float3? = null,
        modelInstance: ModelInstance? = null,
    ) {
        model.first = first ?: model.first
        model.second = second ?: model.second
        model.modelInstance = modelInstance ?: model.modelInstance
    }

    fun updateModelWithLandmark(
        result: FaceLandmarkerResult?,
        typeModel3D: TypeModel3D? = TypeModel3D.GLASSES
    ) {
        val face = result?.faceLandmarks()?.firstOrNull()

        var firstPosition = 0
        var secondPosition = 0
        var model: FilamentModelManager? = null

        when (typeModel3D) {
            TypeModel3D.GLASSES -> {
                firstPosition = 33
                secondPosition = 263
                model = glassModel
            }

            else -> {
            }
        }

        if (model == null) {
            return
        }

        if (face == null) {
            filamentHelper.setVisibility(model, false)
            return
        }

        filamentHelper.setVisibility(model, true)
        val firstLandmark = face.getOrNull(firstPosition) ?: return
        val secondLandmark = face.getOrNull(secondPosition) ?: return
        val first = filamentHelper.convertToFilamentCoordinatesFloat3(
            Float3(
                firstLandmark.x(),
                firstLandmark.y(),
                firstLandmark.z()
            )
        )
        val second = filamentHelper.convertToFilamentCoordinatesFloat3(
            Float3(
                secondLandmark.x(),
                secondLandmark.y(),
                secondLandmark.z()
            )
        )
        updateModel(model, first, second)
        filamentHelper.extractGlassesTransform()
    }
}