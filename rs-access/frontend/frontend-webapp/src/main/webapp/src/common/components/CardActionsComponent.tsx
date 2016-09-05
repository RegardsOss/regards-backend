import * as React from "react"
import SecondaryActionButtonComponent from "./SecondaryActionButtonComponent"
import MainActionButtonComponent from "./MainActionButtonComponent"
import ThemeInjector from "../theme/ThemeInjector"


interface CardActionsProps {
  secondaryButtonLabel?: string | JSX.Element
  secondaryButtonUrl?: string
  secondaryButtonTouchTap?: (event: React.FormEvent) => void

  mainButtonLabel: string | JSX.Element
  mainButtonUrl?: string
  mainButtonTouchTap?: (event: React.FormEvent) => void
  isMainButtonVisible?: boolean
}
/**
 */
class CardActionsComponent extends React.Component<CardActionsProps, any> {


  render (): JSX.Element {
    const isMainButtonVisible = this.props.isMainButtonVisible === true || this.props.isMainButtonVisible === false ?
      this.props.isMainButtonVisible : true
    return (
      <ThemeInjector>
        <CardActionsView
          secondaryButtonLabel={this.props.secondaryButtonLabel}
          secondaryButtonUrl={this.props.secondaryButtonUrl}
          secondaryButtonTouchTap={this.props.secondaryButtonTouchTap}

          mainButtonUrl={this.props.mainButtonUrl}
          mainButtonLabel={this.props.mainButtonLabel}
          mainButtonTouchTap={this.props.mainButtonTouchTap}
          isMainButtonVisible={isMainButtonVisible}

          theme={null}
        />
      </ThemeInjector>
    )
  }
}

// Internal view
interface CardActionsViewProps {
  secondaryButtonLabel: string | JSX.Element
  secondaryButtonUrl: string
  secondaryButtonTouchTap: (event: React.FormEvent) => void

  mainButtonLabel: string | JSX.Element
  mainButtonUrl: string
  mainButtonTouchTap: (event: React.FormEvent) => void
  isMainButtonVisible: boolean

  theme: any
}
class CardActionsView extends React.Component<CardActionsViewProps, any> {

  render (): JSX.Element {

    const styleCardActions = {
      display: "flex",
      flexDirection: "row",
      justifyContent: "flex-end"
    }
    const secondaryButton = this.props.secondaryButtonUrl || this.props.secondaryButtonTouchTap ?
      <SecondaryActionButtonComponent
        label={this.props.secondaryButtonLabel}
        url={this.props.secondaryButtonUrl}
        onTouchTap={this.props.secondaryButtonTouchTap}
      /> : null
    return (
      <div style={styleCardActions}>
        {secondaryButton}
        <MainActionButtonComponent
          label={this.props.mainButtonLabel}
          url={this.props.mainButtonUrl}
          onTouchTap={this.props.mainButtonTouchTap}
          isVisible={this.props.isMainButtonVisible}
        />
      </div>
    )
  }


}
export default CardActionsComponent
