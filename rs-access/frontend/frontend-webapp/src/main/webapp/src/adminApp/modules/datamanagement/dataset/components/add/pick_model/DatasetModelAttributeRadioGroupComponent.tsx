import * as React from "react"
import { RadioButton, RadioButtonGroup } from "material-ui/RadioButton"
import ActionFavorite from "material-ui/svg-icons/action/favorite"
import ActionFavoriteBorder from "material-ui/svg-icons/action/favorite-border"
import { FormattedMessage } from "react-intl"
import { ModelAttribute } from "../../../../model/ModelAttribute"
import TextInputComponent from "../../input/TextInputComponent"

interface ModelAttributeRadioGroupProps {
  attribute: ModelAttribute
  id: string
  staticInput: JSX.Element
}
class DatasetModelAttributeRadioGroupComponent extends React.Component<ModelAttributeRadioGroupProps, any> {
  state: any = {
    radioValue: "dynamic"
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
    console.log(radioValue === "static", this.props.staticInput)
    const computedField = radioValue === "computed" ? (
      <TextInputComponent
        label={<FormattedMessage
            id="datamanagement.dataset.add.1.input.dynamic" />}
      />
    ) : null
    return (
      <div>
        <p>
          <FormattedMessage
            id="datamanagement.dataset.add.1.attribute"
            values={
              {
                name: <i>{attribute.name}</i>
              }
            }
          />
        </p>

        <RadioButtonGroup
          name={"rb-" + id}
          valueSelected={radioValue}
          onChange={this.handleRadioChange}
        >
          <RadioButton
            value="dynamic"
            label={
              <FormattedMessage id="datamanagement.dataset.add.1.attribute.dynamic" />
            }
          />
          <RadioButton
            value="static"
            label={
              <FormattedMessage id="datamanagement.dataset.add.1.attribute.static" />
            }
          />
          <RadioButton
            value="computed"
            label={
              <FormattedMessage id="datamanagement.dataset.add.1.attribute.computed" />
            }
          />
        </RadioButtonGroup>
        {staticField}
        {computedField}

      </div>)
  }
}
export default DatasetModelAttributeRadioGroupComponent
