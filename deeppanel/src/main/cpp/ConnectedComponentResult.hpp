#ifndef ConnectedComponentResult_h
#define ConnectedComponentResult_h

class ConnectedComponentResult {
public:
    int total_clusters;
    int **clusters_matrix;
    int *pixels_per_labels;
};

#endif /* ConnectedComponentResult_h */
