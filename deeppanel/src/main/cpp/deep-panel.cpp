
ConnectedComponentResult remove_small_areas_and_recover_border(
        ConnectedComponentResult connectedComponentResult,
        int width,
        int height) {
    int new_total_clusters = connectedComponentResult.total_clusters;
    int **clusters_matrix = connectedComponentResult.clusters_matrix;
    int *pixels_per_labels = connectedComponentResult.pixels_per_labels;
    bool *label_removed = new bool[width * height];
    int minimumAreaAllowed = width * height * 0.03;
    for (int i = 0; i < width; i++)
        for (int j = 0; j < height; j++) {
            int label = clusters_matrix[i][j];
            if (label != 0) {
                int pixelsPerLabel = pixels_per_labels[label];
                if (pixelsPerLabel < minimumAreaAllowed) {
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