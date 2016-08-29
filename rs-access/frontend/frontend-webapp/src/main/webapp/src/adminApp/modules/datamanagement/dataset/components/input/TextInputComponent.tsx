import * as React from "react"
import TextField from "material-ui/TextField"


interface TextInputProps {
  label: string | JSX.Element
  value?: string
}
/**
 */
class TextInputComponent extends React.Component<TextInputProps, any> {


  constructor (props: TextInputProps) {
    super(props)

    this.state = {
      value: ''
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
  getValue = (): any => {
    const {value} = this.state
    return value
  }
  isDefaultValue = (): boolean => {
    const {value} = this.state
    return value === ''
  }

  render (): JSX.Element {
    const {label, value} = this.props
    return (
      <TextField
        type="text"
        defaultValue={value}
        floatingLabelText={label}
        fullWidth={true}
        onChange={this.handleInputChange}
      />
    )
  }
}

export default TextInputComponent
