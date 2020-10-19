#ifndef deep_panel_hpp
#define deep_panel_hpp

#include "DeepPanelResult.hpp"

DeepPanelResult extract_panels_info(int **labeled_matrix,
                                    int width,
                                    int height, float scale,
                                    int original_image_width,
                                    int original_image_height);
#endif /* deep_panel_h */
