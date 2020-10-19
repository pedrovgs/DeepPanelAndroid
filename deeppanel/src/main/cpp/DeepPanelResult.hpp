#ifndef DeepPanelResult_hpp
#define DeepPanelResult_hpp

#include <stdio.h>
#include "Panel.hpp"
#include "ConnectedComponentResult.hpp"

class DeepPanelResult {
public:
    ConnectedComponentResult connected_components;
    Panel *panels;
};

#endif /* DeepPanelResult_hpp */
