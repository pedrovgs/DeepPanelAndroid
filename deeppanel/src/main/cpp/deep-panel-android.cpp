#include <jni.h>
#import "connected-components.cpp"
#import "deep-panel.cpp"

jint map_predicted_row_to_label(JNIEnv *env, jobjectArray prediction, int i, int j) {
    auto x = (jobjectArray) env->GetObjectArrayElement(prediction, i);
    auto y = (jfloatArray) env->GetObjectArrayElement(x, j);
    jfloat *pixel_prediction = env->GetFloatArrayElements(y, 0);
    jfloat background = pixel_prediction[0];
    jfloat border = pixel_prediction[1];
    jfloat content = pixel_prediction[2];
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
    jclass int_array_class = env->FindClass("[I");
    jobjectArray labels_array = env->NewObjectArray(width, int_array_class, nullptr);
    for (int i = 0; i < width; i++) {
        jintArray int_array = env->NewIntArray(height);
        env->SetIntArrayRegion(int_array, 0, height, matrix[i]);
        env->SetObjectArrayElement(labels_array, i, int_array);
    }
    return labels_array;
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_github_pedrovgs_deeppanel_NativeConnectedComponentLabeling_transformPredictionIntoLabels
        (
                JNIEnv *env,
                jobject /* this */, jobjectArray prediction) {
    auto first_item = (jobjectArray) env->GetObjectArrayElement(prediction, 0);
    jsize width = env->GetArrayLength(prediction);
    jsize height = env->GetArrayLength(first_item);
    int **labeled_matrix = new int *[height];
    for (int i = 0; i < width; i++) {
        labeled_matrix[i] = new int[width];
        for (int j = 0; j < height; j++) {
            // j and i indexes order is changed on purpose because the original matrix
            // is rotated when reading the values.
            labeled_matrix[i][j] = map_predicted_row_to_label(env, prediction, j, i);
        }
    }
    ConnectedComponentResult result = extract_panels_info(labeled_matrix, width, height);
    int **connectedComponentsMatrix = result.clusters_matrix;
    jobjectArray java_ints_array = intArrayToJavaIntArray(env, connectedComponentsMatrix, width,
                                                          height);
    return java_ints_array;
}


