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

jobjectArray int_array_to_java_array(JNIEnv *env, int **matrix, int width, int height) {
    jclass int_array_class = env->FindClass("[I");
    jobjectArray labels_array = env->NewObjectArray(width, int_array_class, nullptr);
    for (int i = 0; i < width; i++) {
        jintArray int_array = env->NewIntArray(height);
        env->SetIntArrayRegion(int_array, 0, height, matrix[i]);
        env->SetObjectArrayElement(labels_array, i, int_array);
    }
    return labels_array;
}

jobject panel_to_java_raw_panel(JNIEnv *env, Panel panel) {
    jclass raw_panel_class = env->FindClass("com/github/pedrovgs/deeppanel/RawPanel");
    jmethodID constructor = env->GetMethodID(raw_panel_class, "<init>", "()V");
    jobject result_objc = env->NewObject(raw_panel_class, constructor);
    jmethodID set_left = env->GetMethodID(raw_panel_class, "setLeft", "(I)V");
    env->CallVoidMethod(result_objc, set_left, panel.left);
    jmethodID set_top = env->GetMethodID(raw_panel_class, "setTop", "(I)V");
    env->CallVoidMethod(result_objc, set_top, panel.top);
    jmethodID set_right = env->GetMethodID(raw_panel_class, "setRight", "(I)V");
    env->CallVoidMethod(result_objc, set_right, panel.right);
    jmethodID set_bottom = env->GetMethodID(raw_panel_class, "setBottom", "(I)V");
    env->CallVoidMethod(result_objc, set_bottom, panel.bottom);
    return result_objc;
}

jobjectArray result_to_java_raw_panels_info(JNIEnv *env, DeepPanelResult result) {
    int number_of_panels = result.connected_components.total_clusters;
    jclass panel_class = env->FindClass("com/github/pedrovgs/deeppanel/RawPanel");
    jobjectArray panels_array = env->NewObjectArray(number_of_panels, panel_class, nullptr);
    for (int i = 0; i < number_of_panels; i++) {
        Panel panel = result.panels[i];
        jobject raw_panel = panel_to_java_raw_panel(env, panel);
        env->SetObjectArrayElement(panels_array, i, raw_panel);
    }
    return panels_array;
}

jobject compose_java_result(JNIEnv *env, DeepPanelResult result, int width, int height) {
    int **connectedComponentsMatrix = result.connected_components.clusters_matrix;
    jobjectArray java_ints_array = int_array_to_java_array(env, connectedComponentsMatrix, width,
                                                           height);
    jobjectArray panels = result_to_java_raw_panels_info(env, result);

    jclass result_class = env->FindClass("com/github/pedrovgs/deeppanel/RawPanelsInfo");
    jmethodID constructor = env->GetMethodID(result_class, "<init>", "()V");
    jobject result_objc = env->NewObject(result_class, constructor);
    jmethodID set_int_arrays_method = env->GetMethodID(result_class, "setConnectedAreas", "([[I)V");
    env->CallVoidMethod(result_objc, set_int_arrays_method, java_ints_array);

    jmethodID set_panels_method = env->GetMethodID(result_class, "setPanels", "([Lcom/github/pedrovgs/deeppanel/RawPanel;)V");
    env->CallVoidMethod(result_objc, set_panels_method, panels);
    return result_objc;
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_github_pedrovgs_deeppanel_NativeDeepPanel_extractPanelsInfo
        (
                JNIEnv *env,
                jobject /* this */,
                jobjectArray prediction,
                jfloat scale,
                jint original_image_width,
                jint original_image_height) {
    auto first_item = (jobjectArray) env->GetObjectArrayElement(prediction, 0);
    jsize width = env->GetArrayLength(prediction);
    jsize height = env->GetArrayLength(first_item);
    int **labeled_matrix = new int *[height];
    for (int i = 0; i < width; i++) {
        labeled_matrix[i] = new int[width];
        for (int j = 0; j < height; j++) {
            // j and i indexes order is changed on purpose because the original matrix
            // is rotated when reading the values.
            // TODO: Fix weird error here returning a null row randomly "map_predicted_row_to_label"
            // should be broken.
            labeled_matrix[i][j] = map_predicted_row_to_label(env, prediction, j, i);
        }
    }
    DeepPanelResult result = extract_panels_info(labeled_matrix, width, height, scale, original_image_width, original_image_height);
    return compose_java_result(env, result, width, height);
}
