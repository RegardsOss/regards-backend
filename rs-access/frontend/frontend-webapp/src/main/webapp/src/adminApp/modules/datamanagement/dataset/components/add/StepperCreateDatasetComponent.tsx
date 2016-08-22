import * as React from "react"
import { Step, Stepper, StepLabel } from "material-ui/Stepper"

interface StepperCreateDatasetProps {
  getStepperIndex: () => number
}
/**
 */
export default class StepperCreateDatasetComponent extends React.Component<StepperCreateDatasetProps, any> {

  getStepperIndex = () => {
    return this.props.getStepperIndex()
  }

  render (): JSX.Element {
    return (
      <div style={{width: '100%', maxWidth: 700, margin: 'auto'}}>
        <Stepper activeStep={this.getStepperIndex()}>
          <Step>
            <StepLabel>Fill dataset attributes</StepLabel>
          </Step>
          <Step>
            <StepLabel>Configure datasource</StepLabel>
          </Step>
          <Step>
            <StepLabel>Done</StepLabel>
          </Step>
        </Stepper>
      </div>
    )
  }
}

/*
 const mapStateToProps = (state: any, ownProps: any) => {
 }
 const mapDispatchToProps = (dispatch: any) => ({
 })
 export default connect<{}, {}, DatasetCreateProps>(mapStateToProps, mapDispatchToProps)(DatasetCreateContainer)
 */
