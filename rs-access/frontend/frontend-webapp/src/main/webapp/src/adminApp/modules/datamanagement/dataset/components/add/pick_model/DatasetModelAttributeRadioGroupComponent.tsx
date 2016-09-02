import * as React from "react"
import { RadioButton, RadioButtonGroup } from "material-ui/RadioButton"
import { FormattedMessage, intlShape } from "react-intl"
import { ModelAttribute } from "../../../../datasetmodel/ModelAttribute"
import TextInputComponent from "../../input/TextInputComponent"

interface ModelAttributeRadioGroupProps {
  attribute: ModelAttribute
  id: string
  staticInput: JSX.Element
}
class DatasetModelAttributeRadioGroupComponent extends React.Component<ModelAttributeRadioGroupProps, any> {
  static contextTypes: Object = {
    intl: intlShape,
    muiTheme: React.PropTypes.object.isRequired
  }
  context: {
    intl: any,
    muiTheme: any
  }

  state: any = {
    radioValue: "static"
  }

  handleRadioChange = (event: React.FormEvent, value: string): void => {
    this.setState({
      radioValue: value
    })
  }

  render (): JSX.Element {
    const {attribute, id} = this.props
    const {radioValue} = this.state
    const staticField = radioValue === "static" ? this.props.staticInput : null
    const computedField = radioValue === "computed" ? (
      <TextInputComponent
        label={<FormattedMessage
            id="datamanagement.dataset.add.1.input.dynamic" />}
      />
    ) : null
    return (
      <div>
        <div className="row">
          <div className="col-sm-100">
            <FormattedMessage
              id="datamanagement.dataset.add.1.attribute"
              values={{
                name: <i>{attribute.name}</i>
              }}
            />

          </div>
          <div className="col-sm-20">

            <br />
            <RadioButtonGroup
              name={"rb-" + id}
              valueSelected={radioValue}
              onChange={this.handleRadioChange}
            >
              <RadioButton
                value="static"
                label={
              this.context.intl.formatMessage({id: "datamanagement.dataset.add.1.attribute.static"})
            }
              />
              <RadioButton
                value="computed"
                label={
              this.context.intl.formatMessage({id: "datamanagement.dataset.add.1.attribute.computed"})
            }
              />
            </RadioButtonGroup>

            <br />
            <br />
          </div>
          <div className="col-sm-80">
            {staticField}
            {computedField}
          </div>
        </div>
      </div>
    )
  }
}
export default DatasetModelAttributeRadioGroupComponent
