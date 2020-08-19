void depth_first_search(int **connected_components, int x, int y, int current_label, int **matrix,
                        int width, int height) {
    int current_item = matrix[x][y];
    if (connected_components[x][y] || current_item > 1)
        return; // already labeled

    // mark the current cell
    connected_components[x][y] = current_label;

    if (x < width)
        depth_first_search(connected_components,
                           x + 1,
                           y,
                           current_label, matrix, width, height);
    if (x > 0)
        depth_first_search(connected_components,
                           x - 1,
                           y,
                           current_label, matrix, width, height);
    if (y < height)
        depth_first_search(connected_components,
                           x,
                           y + 1,
                           current_label, matrix, width, height);
    if (y > 0)
        depth_first_search(connected_components,
                           x,
                           y - 1,
                           current_label, matrix, width, height);
}

int **find_components(int **matrix, int width, int height) {
    int **connected_components = 0;
    connected_components = new int *[height];
    int initial_label = 1;
    for (int i = 0; i < width; ++i) {
        if (connected_components[i] == NULL) {
            connected_components[i] = new int[width];
        }
        for (int j = 0; j < height; ++j) {
            if (!connected_components[i][j] && matrix[i][j]) {
                depth_first_search(
                        connected_components,
                        i,
                        j,
                        ++initial_label, matrix, width, height);
            }
        }
    }
    return connected_components;
}