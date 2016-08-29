import * as React from "react"
import TextField from "material-ui/TextField"


interface IntegerInputProps {
  label: string
  value?: number
}
/**
 */
class IntegerInputComponent extends React.Component<IntegerInputProps, any> {


  constructor (props: IntegerInputProps) {
    super(props)

    this.state = {
      value: null
    }
    if (props.value) {
      this.state.value = props.value
    }
  }

  handleInputChange = (event: React.FormEvent): any => {
    const newLabel = (event.target as any).value
    this.setState({
      value: newLabel
    })
  }
  getValue = (): number => {
    const {value} = this.state
    return parseInt(value)
  }
  isDefaultValue = (): boolean => {
    const {value} = this.state
    return value === null
  }

  render (): JSX.Element {
    const {label, value} = this.props
    return (
      <TextField
        type="number"
        defaultValue={value}
        floatingLabelText={label}
        fullWidth={true}
        onChange={this.handleInputChange}
      />
    )
  }
}

export default IntegerInputComponent
