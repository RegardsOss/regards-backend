import * as React from "react"
import TextField from "material-ui/TextField"


class UserForm extends React.Component<any, any> {

  render(): any {
    return (
      <form>
        <TextField
          floatingLabelText="Name"
        /><br />
        <TextField
          floatingLabelText="Password"
          type="password"
        /><br />
        <TextField
          floatingLabelText="Password (confirm)"
          type="password"
        />
      </form>
    )
  }

}

export default UserForm
