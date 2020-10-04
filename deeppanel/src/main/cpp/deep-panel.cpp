class Panel {
public:
    int left;
    int bottom;
    int right;
    int top;
};

class DeepPanelResult {
public:
    ConnectedComponentResult connected_components;
    Panel *panels;
};

ConnectedComponentResult remove_small_areas_and_recover_border(
        ConnectedComponentResult connected_component_result,
        int width,
        int height) {
    int new_total_clusters = connected_component_result.total_clusters;
    int **clusters_matrix = connected_component_result.clusters_matrix;
    int *pixels_per_labels = connected_component_result.pixels_per_labels;
    int image_size = width * height;
    int max_allowed_different_clusters = 2000;
    bool *label_removed = new bool[max_allowed_different_clusters];
    for (int i = 0; i < image_size; i++) {
        label_removed[i] = false;
    }
    int min_allowed_area = image_size * 0.03;
    for (int i = 0; i < width; i++)
        for (int j = 0; j < height; j++) {
            int label = clusters_matrix[i][j];
            if (label != 0) {
                int pixelsPerLabel = pixels_per_labels[label];
                if (pixelsPerLabel < min_allowed_area) {
                    clusters_matrix[i][j] = 0;
                    if (!label_removed[label]) {
                        new_total_clusters--;
                        label_removed[label] = true;
                    }
                }
            }
        }
    free(label_removed);
    ConnectedComponentResult result;
    result.clusters_matrix = clusters_matrix;
    result.total_clusters = new_total_clusters;
    result.pixels_per_labels = pixels_per_labels;
    return result;
}

#define max(a, b) (a>b?a:b)
#define min(a, b) (a>b?b:a)

int apply_scale_and_add_border(int position, float scale, int border) {
    float float_position = (float) position;
    float scaled_position = float_position * scale;
    return ((int) scaled_position) + border;
}

DeepPanelResult
extract_panels_data(ConnectedComponentResult connected_components_result,
                    int width,
                    int height,
                    float scale,
                    jint original_image_width,
                    jint original_image_height) {
    int number_of_panels = connected_components_result.total_clusters;
    int current_normalized_label = 0;
    int *normalized_labels = new int[width * height];
    int *min_x_values = new int[number_of_panels + 1];
    int *max_x_values = new int[number_of_panels + 1];
    int *min_y_values = new int[number_of_panels + 1];
    int *max_y_values = new int[number_of_panels + 1];
    for (int i = 0; i < number_of_panels + 1; i++) {
        min_x_values[i] = INT_MAX;
        max_x_values[i] = INT_MIN;
        min_y_values[i] = INT_MAX;
        max_y_values[i] = INT_MIN;
    }
    int **cluster_matrix = connected_components_result.clusters_matrix;
    for (int i = 0; i < width; i++)
        for (int j = 0; j < height; j++) {
            int raw_label = cluster_matrix[i][j];
            if (raw_label != 0) {
                if (normalized_labels[raw_label] == 0) {
                    current_normalized_label++;
                    normalized_labels[raw_label] = current_normalized_label;
                }
                int normalized_label = normalized_labels[raw_label];
                min_x_values[normalized_label] = min(min_x_values[normalized_label], i);
                max_x_values[normalized_label] = max(max_x_values[normalized_label], i);
                min_y_values[normalized_label] = min(min_y_values[normalized_label], j);
                max_y_values[normalized_label] = max(max_y_values[normalized_label], j);
            }
        }
    Panel *panels = new Panel[number_of_panels];
    int horizontal_correction = 0;
    int vertical_correction = 0;
    if (original_image_width < original_image_height) {
        horizontal_correction = ((width * scale) - original_image_width) / 2;
    } else {
        vertical_correction = ((height * scale) - original_image_height) / 2;
    }
    int border = 30;
    for (int i = 1; i <= number_of_panels; i++) {
        Panel panel;
        panel.left = apply_scale_and_add_border(min_x_values[i], scale, -border) - horizontal_correction;
        panel.top = apply_scale_and_add_border(min_y_values[i], scale, -border) - vertical_correction;
        panel.right = apply_scale_and_add_border(max_x_values[i], scale, border) - horizontal_correction;
        panel.bottom = apply_scale_and_add_border(max_y_values[i], scale, border) - vertical_correction;
        panels[i - 1] = panel;
    }
    free(min_x_values);
    free(max_x_values);
    free(min_y_values);
    free(max_y_values);
    DeepPanelResult deep_panel_result;
    deep_panel_result.connected_components = connected_components_result;
    deep_panel_result.panels = panels;
    return deep_panel_result;
}

DeepPanelResult extract_panels_info(int **labeled_matrix,
                                    int width,
                                    int height, float scale,
                                    jint original_image_width,
                                    jint original_image_height) {
    ConnectedComponentResult improvedAreasResult = find_components(labeled_matrix, width, height);
    improvedAreasResult = remove_small_areas_and_recover_border(improvedAreasResult, width, height);
    return extract_panels_data(improvedAreasResult, width, height, scale, original_image_width,
                               original_image_height);
}
