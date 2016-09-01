import * as React from "react"
import { Card, CardHeader, CardText } from "material-ui/Card"
import { FormattedMessage } from "react-intl"
import TextField from "material-ui/TextField"
import SelectField from "material-ui/SelectField"
import MenuItem from "material-ui/MenuItem"
import CardActionsComponent from "../../../../../../common/components/CardActionsComponent"

interface ConnectionCreateProps {
  getCancelUrl: () => string
  handleNextStep: (name: string) => void
}
/**
 */
class ConnectionCreateComponent extends React.Component<ConnectionCreateProps, any> {

  constructor (props: ConnectionCreateProps) {
    super(props)
    this.state = {
      label: "",
      openCreateParameterModal: false
    }
  }

  handleSaveButton = (event: React.FormEvent) => {
    return this.props.handleNextStep(this.state.label)
  }
  handleCancelUrl = (): string => {
    return this.props.getCancelUrl()
  }

  render (): JSX.Element {
    return (
      <Card
        initiallyExpanded={true}>
        <CardHeader
          title={<FormattedMessage id="datamanagement.connection.create.header"/>}
          actAsExpander={true}
          showExpandableButton={false}
        />
        <CardText>

          <SelectField
            floatingLabelText="Select the type of connection"
            value={3}
            fullWidth={true}
          >
            <MenuItem value={1} primaryText="Plugin oracle"/>
            <MenuItem value={2} primaryText="Plugin mongodb"/>
            <MenuItem value={3} primaryText="Plugin cassandra"/>
          </SelectField>
          <TextField
            type="text"
            floatingLabelText={<FormattedMessage id="Label"/>}
            fullWidth={true}
          />
          <TextField
            type="text"
            floatingLabelText={<FormattedMessage id="Username"/>}
            fullWidth={true}
          />
          <TextField
            type="password"
            floatingLabelText={<FormattedMessage id="Password"/>}
            fullWidth={true}
          />
          <TextField
            type="text"
            floatingLabelText={<FormattedMessage id="URL"/>}
            fullWidth={true}
          />
          <TextField
            type="text"
            floatingLabelText={<FormattedMessage id="Port"/>}
            fullWidth={true}
          />

          <CardActionsComponent
            secondaryButtonUrl={this.handleCancelUrl()}
            secondaryButtonLabel={
              <FormattedMessage
                id="datamanagement.connection.add.action.cancel"
              />
            }

            mainButtonTouchTap={this.handleSaveButton}
            mainButtonLabel={
              <FormattedMessage
                id="datamanagement.connection.add.action.test"
              />
            }
          />
        </CardText>
      </Card>
    )
  }
}
export default ConnectionCreateComponent
