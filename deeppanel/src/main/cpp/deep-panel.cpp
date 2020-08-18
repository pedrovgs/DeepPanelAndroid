#include <jni.h>
#include <string>

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

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_github_pedrovgs_deeppanel_NativeConnectedComponentLabeling_transformPredictionIntoLabels
        (
                JNIEnv *env,
                jobject /* this */, jobjectArray prediction) {
    auto firstItem = (jobjectArray) env->GetObjectArrayElement(prediction, 0);
    jsize width = env->GetArrayLength(prediction);
    jsize height = env->GetArrayLength(firstItem);
    jclass intArrayCLass = env->FindClass("[I");
    jobjectArray labelsArray = env->NewObjectArray(width, intArrayCLass, NULL);
    for (int i = 0; i < width; i++) {
        jintArray intArray = env->NewIntArray(height);
        jint labelsArrayPerRow[height];
        for (int j = 0; j < height; j++) {
            labelsArrayPerRow[j] = mapPredictedRowToLabel(env, prediction, i, j);
        }
        env->SetIntArrayRegion(intArray, 0, height, labelsArrayPerRow);
        env->SetObjectArrayElement(labelsArray, i, intArray);
    }
    return labelsArray;
}

