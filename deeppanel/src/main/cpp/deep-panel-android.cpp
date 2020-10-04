#include <jni.h>
#import "connected-components.cpp"
#import "deep-panel.cpp"

jint mapPredictedRowToLabel(JNIEnv *env, jobjectArray prediction, int i, int j) {
    auto x = (jobjectArray) env->GetObjectArrayElement(prediction, i);
    auto y = (jfloatArray) env->GetObjectArrayElement(x, j);
    jfloat *pixelPrediction = env->GetFloatArrayElements(y, 0);
    jfloat background = pixelPrediction[0];
    jfloat border = pixelPrediction[1];
    jfloat content = pixelPrediction[2];
    if (background >= content && background > border) {
        return 0;
    } else if (border >= background && border >= content) {
        return 0; // The original label should be 1 but we need this value to be 0
        // because the ccl algorithm uses 0 and 1 as values. 0 is used for the background.
    } else {
        return 1; // The original label should be 1 but we need this value to be 0
        // because the ccl algorithm uses 0 and 1 as values. 1 is used for the content.
    }
}

jobjectArray intArrayToJavaIntArray(JNIEnv *env, int **matrix, int width, int height) {
    jclass intArrayCLass = env->FindClass("[I");
    jobjectArray labelsArray = env->NewObjectArray(width, intArrayCLass, nullptr);
    for (int i = 0; i < width; i++) {
        jintArray intArray = env->NewIntArray(height);
        env->SetIntArrayRegion(intArray, 0, height, matrix[i]);
        env->SetObjectArrayElement(labelsArray, i, intArray);
    }
    return labelsArray;
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_github_pedrovgs_deeppanel_NativeConnectedComponentLabeling_transformPredictionIntoLabels
        (
                JNIEnv *env,
                jobject /* this */, jobjectArray prediction) {
    auto firstItem = (jobjectArray) env->GetObjectArrayElement(prediction, 0);
    jsize width = env->GetArrayLength(prediction);
    jsize height = env->GetArrayLength(firstItem);
    int **labeledMatrix;
    labeledMatrix = new int *[height];
    for (int i = 0; i < width; i++) {
        labeledMatrix[i] = new int[width];
        for (int j = 0; j < height; j++) {
            // j and i indexes order is changed on purpose because the original matrix
            // is rotated when reading the values.
            labeledMatrix[i][j] = mapPredictedRowToLabel(env, prediction, j, i);
        }
    }
    ConnectedComponentResult result = find_components(labeledMatrix, width, height);
    ConnectedComponentResult improved_areas_result = remove_small_areas_and_recover_border(result, width, height);
    int **connectedComponentsMatrix = improved_areas_result.clusters_matrix;
    jobjectArray javaIntsArray = intArrayToJavaIntArray(env, connectedComponentsMatrix, width, height);
    return javaIntsArray;
}


