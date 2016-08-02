import * as React from 'react'
import { connect } from 'react-redux'
import * as _ from 'lodash'
import * as actions from '../actions'

import 'react-grid-layout/css/styles.css'
import 'react-resizable/css/styles.css'

var WidthProvider = require('react-grid-layout').WidthProvider
var ResponsiveReactGridLayout = require('react-grid-layout').Responsive
ResponsiveReactGridLayout = WidthProvider(ResponsiveReactGridLayout)

interface LayoutProps {
  layout?: any,
  setLayout?: (layout: any)=>void,
  sideBarElement?: JSX.Element,
  appBarElement?: JSX.Element,
  contentElements?: Array<JSX.Element>, // An array of react components
}

class Layout extends React.Component<LayoutProps, any> {
  constructor() {
    super()
    this.onLayoutChange = this.onLayoutChange.bind(this)
    this.state = { margin: 10 }
  }

  onLayoutChange(layout:any, layouts:any) {
    // console.log('Layout changed')
    // this.state.layouts = layouts
    // this.props.setLayout(layouts)
  }

  render() {

    return (
      <div>
        <ResponsiveReactGridLayout
          className='layout'
          breakpoints={{lg: 1200, md: 996, sm: 768, xs: 480, xxs: 0}}
          cols={{lg: 12, md: 10, sm: 6, xs: 4, xxs: 2}}
          rowHeight={30}
          margin={[this.state.margin, this.state.margin]}
          style={{
            backgroundColor:'#ECEFF1',
            position: 'absolute',
            height: '100%',
            width: '100%'
          }}
          layouts={this.props.layout}
          onLayoutChange={this.onLayoutChange}
          isDraggable={false}
          isResizable={false}
          {...this.props}
          >
          {this.props.children}
        </ResponsiveReactGridLayout>
      </div>
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
