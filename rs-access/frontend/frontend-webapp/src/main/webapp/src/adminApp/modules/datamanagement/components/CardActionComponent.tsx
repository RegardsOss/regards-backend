import * as React from "react"
import CancelButtonComponent from "./CancelButtonComponent"
import MainButtonComponent from "./MainButtonComponent"


interface CardActionsProps {
  secondaryButtonUrl: string
  secondaryButtonLabel: string | JSX.Element

  mainButtonUrl: string
  mainButtonLabel: string | JSX.Element
}
/**
 */
class CardActionsComponent extends React.Component<CardActionsProps, any> {


  render (): JSX.Element {

    return (
      // Todo: inject theme
      <CardActionsView
        secondaryButtonUrl={this.props.secondaryButtonUrl}
        secondaryButtonLabel={this.props.secondaryButtonLabel}
        mainButtonUrl={this.props.mainButtonUrl}
        mainButtonLabel={this.props.mainButtonLabel}
        theme={null}
      />
    )
  }
}

// Internal view
interface CardActionsViewProps {
  secondaryButtonUrl: string
  secondaryButtonLabel: string | JSX.Element

  mainButtonUrl: string
  mainButtonLabel: string | JSX.Element
  theme: any
}
class CardActionsView extends React.Component<CardActionsViewProps, any> {


  render (): JSX.Element {
    const {secondaryButtonUrl, secondaryButtonLabel, mainButtonLabel, mainButtonUrl, theme} = this.props

    // Todo : move to theme
    const styleCardActions = {
      display: "flex",
      flexDirection: "row",
      justifyContent: "flex-end"
    }
    return (
      <div style={styleCardActions}>
        <CancelButtonComponent
          label={secondaryButtonLabel}
          url={secondaryButtonUrl}
        />
        <MainButtonComponent
          label={mainButtonLabel}
          url={mainButtonUrl}
        />
      </div>
    )
  }
}


export default CardActionsComponent
