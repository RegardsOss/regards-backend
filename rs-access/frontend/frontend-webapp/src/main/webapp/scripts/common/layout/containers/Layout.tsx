import * as React from "react";
import { connect } from "react-redux";
import * as actions from "../actions";
import "react-grid-layout/css/styles.css";
import "react-resizable/css/styles.css";

const WidthProvider = require ('react-grid-layout').WidthProvider
let ResponsiveReactGridLayout = require ('react-grid-layout').Responsive
ResponsiveReactGridLayout = WidthProvider (ResponsiveReactGridLayout)

interface LayoutProps {
  style?: any,
  layout?: any,
  setLayout?: (layout: any) => void,
}

class Layout extends React.Component<LayoutProps, any> {
  constructor() {
    super ()
    this.onLayoutChange = this.onLayoutChange.bind (this)
    this.state = {margin: 10}
  }

  onLayoutChange(layout: any, layouts: any): any {
    // console.log('Layout changed')
    // this.state.layouts = layouts
    // this.props.setLayout(layouts)
  }

  render(): JSX.Element {
    const {style, layout} = this.props;
    const defaultStyle = {
      backgroundColor:'#ECEFF1', // #ECEFF1
      height: "100vh"
    };
    let layoutStyle = style ? Object.assign ({}, defaultStyle, style) : defaultStyle;

    return (
      <ResponsiveReactGridLayout
        {...this.props}
        className='layout'
        breakpoints={{lg: 1200, md: 996, sm: 768, xs: 480, xxs: 0}}
        cols={{lg: 12, md: 12, sm: 12, xs: 12, xxs: 12}}
        rowHeight={30}
        margin={[this.state.margin, this.state.margin]}
        style={layoutStyle}
        layouts={layout}
        onLayoutChange={this.onLayoutChange}
        verticalCompact={false} // If true, the layout will compact vertically
        autoSize={false} // If true, the container height swells and contracts to fit contents
        isDraggable={false}
        isResizable={true}
      >
        {this.props.children}
      </ResponsiveReactGridLayout>
    )
  }
}

const mapStateToProps = (state: any) => ({
  layout: state.common.layout
})
const mapDispatchToProps = (dispatch: any) => ({
  setLayout: (layout: any) => dispatch (actions.setLayout (layout))
})
export default connect<{}, {}, LayoutProps> (mapStateToProps, mapDispatchToProps) (Layout)
