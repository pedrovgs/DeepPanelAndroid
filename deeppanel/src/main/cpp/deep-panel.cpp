
ConnectedComponentResult remove_small_areas_and_recover_border(
        ConnectedComponentResult connected_component_result,
        int width,
        int height) {
    int new_total_clusters = connected_component_result.total_clusters;
    int **clusters_matrix = connected_component_result.clusters_matrix;
    int *pixels_per_labels = connected_component_result.pixels_per_labels;
    bool *label_removed = new bool[width * height];
    int min_allowed_area = width * height * 0.03;
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
    ConnectedComponentResult result;
    result.clusters_matrix = clusters_matrix;
    result.total_clusters = new_total_clusters;
    result.pixels_per_labels = pixels_per_labels;
    return result;
}

ConnectedComponentResult extract_panels_info(int **labeled_matrix, int width, int height) {
    ConnectedComponentResult result = find_components(labeled_matrix, width, height);
    return remove_small_areas_and_recover_border(result, width, height);
}