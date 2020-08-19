const int dx[] = {+1, 0, -1, 0};
const int dy[] = {0, +1, 0, -1};

void depth_first_search(int **connected_components, int x, int y, int current_label, int **matrix,
                        int width, int height) {
    if (x < 0 || x == width) return;
    if (y < 0 || y == height) return;
    int current_item = matrix[x][y];
    if (connected_components[x][y])  // already labeled
        return;
    if (current_item > 1) { // border marked as labeled with the border value
        connected_components[x][y] = 1;
        return;
    }

    // mark the current cell
    connected_components[x][y] = current_label;

    for (int direction = 0; direction < 4; ++direction)
        depth_first_search(connected_components, x + dx[direction], y + dy[direction],
                           current_label, matrix, width, height);
}

int **find_components(int **matrix, int width, int height) {
    int **connected_components = nullptr;
    connected_components = new int *[height];
    int initial_label = 1;
    for (int i = 0; i < width; ++i) {
        connected_components[i] = new int[width];
    }
    for (int i = 0; i < width; ++i) {
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