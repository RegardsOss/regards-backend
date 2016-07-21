import * as React from 'react'
import { connect } from 'react-redux'
import * as _ from 'lodash'
import * as actions from '../actions'

var WidthProvider = require('react-grid-layout').WidthProvider
var ResponsiveReactGridLayout = require('react-grid-layout').Responsive
ResponsiveReactGridLayout = WidthProvider(ResponsiveReactGridLayout)

interface LayoutProps {
  layout?: any,
  setLayout?: (layout: any)=>void
}

class Layout extends React.Component<LayoutProps, any> {
  constructor() {
    super()
    this.onLayoutChange = this.onLayoutChange.bind(this)
    // this.state = { layouts: props.layouts }
  }

  onLayoutChange(layout:any, layouts:any) {
    // console.log('Layout changed')
    // this.state.layouts = layouts
    // this.props.setLayout(layouts)
  }

  render() {
    return (
      <ResponsiveReactGridLayout
        className='layout'
        cols={{lg: 12, md: 10, sm: 6, xs: 4, xxs: 2}}
        rowHeight={30}
        style={{
          backgroundColor:'#00bcd4',
          position: 'absolute',
          height: '100%',
          width: '100%'
        }}
        layouts={this.props.layout}
        onLayoutChange={this.onLayoutChange}
        isDraggable={true}
        isResizable={true}
        {...this.props}
        >
        {this.props.children}
      </ResponsiveReactGridLayout>
    )
  }
}

const mapStateToProps = (state:any) => ({
  layout: state.common.layout
})
const mapDispatchToProps = (dispatch:any) => ({
  setLayout: (layout: any) => dispatch(actions.setLayout(layout))
})
export default connect<{}, {}, LayoutProps>(mapStateToProps,mapDispatchToProps)(Layout)
