#include <jni.h>
#include <string>
#import "connected-components.cpp"

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
        return 1;
    } else {
        return 2;
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
            labeledMatrix[i][j] = mapPredictedRowToLabel(env, prediction, j, i);
        }
    }
    int **connectedComponentsMatrix = find_components(labeledMatrix, width, height);
    jobjectArray javaIntsArray = intArrayToJavaIntArray(env, connectedComponentsMatrix, width, height);
    return javaIntsArray;
}


