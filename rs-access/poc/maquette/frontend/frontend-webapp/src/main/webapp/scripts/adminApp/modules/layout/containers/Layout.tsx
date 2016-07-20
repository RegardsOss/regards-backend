import * as React from 'react'
var PureRenderMixin = require('react/lib/ReactComponentWithPureRenderMixin');
var WidthProvider = require('react-grid-layout').WidthProvider;
var ResponsiveReactGridLayout = require('react-grid-layout').Responsive;
ResponsiveReactGridLayout = WidthProvider(ResponsiveReactGridLayout);
import * as _ from 'lodash'

interface LayoutProps {
  className: string,
  cols: Object,
  rowHeight: number
}

class Layout extends React.Component<LayoutProps, any> {
  constructor() {
    super()
    this.onLayoutChange = this.onLayoutChange.bind(this)
    this.state = {
      layouts: {
        lg: [
          {i: '1', x: 0, y: 0, w: 1, h: 12},
          {i: '2', x: 1, y: 1, w: 11, h: 12}
        ],
        md: [
          {i: '1', x: 0, y: 0, w: 1, h: 12},
          {i: '2', x: 1, y: 1, w: 9, h: 12}
        ],
        sm: [
          {i: '1', x: 0, y: 0, w: 12, h: 2},
          {i: '2', x: 0, y: 1, w: 12, h: 9}
        ],
        xs: [
          {i: '1', x: 0, y: 0, w: 12, h: 2},
          {i: '2', x: 0, y: 1, w: 12, h: 9}
        ],
        xxs: [
          {i: '1', x: 0, y: 0, w: 12, h: 2},
          {i: '2', x: 0, y: 1, w: 12, h: 9}
        ]
      }
    }
  }

  // resetLayout() {
  //   this.setState({layouts: {}});
  // },

  onLayoutChange(layout:any, layouts:any) {
    // saveToLS('layouts', layouts);
    // this.setState({layouts});
    this.state.layouts = layouts
    // console.log('Layout changed')
    // this.props.onLayoutChange(layout, layouts);
  }

  // getGridItems() {
  //   return _.map(this.props.children, (child: any, i: any) =>
  //     <div key={i+''} style={{backgroundColor:'#e0e0e0'}}>
  //       {child}
  //     </div>
  //   )
  // }

  render() {
    return (
      <ResponsiveReactGridLayout
        style={{
          backgroundColor:'#ff4081',
          position: 'absolute',
          height: '100%',
          width: '100%'
        }}
        ref="rrgl" {...this.props}
        layouts={this.state.layouts}
        onLayoutChange={this.onLayoutChange}
        isDraggable={false}
        isResizable={false}
        >
        <div key='1' style={{
          backgroundColor:'#00bcd4',
          height: '100%'
        }}></div>
        <div key='2' style={{backgroundColor:'#FFCA28'}}></div>
      </ResponsiveReactGridLayout>
    )
  }
}

export default Layout
