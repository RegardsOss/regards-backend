import * as React from "react"
import CancelButtonComponent from "./CancelButtonComponent"
import MainButtonComponent from "./MainButtonComponent"
import ThemeInjector from "../../../../common/theme/ThemeInjector"


interface CardActionsProps {
  secondaryButtonLabel: string | JSX.Element
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
    const {
      secondaryButtonUrl, secondaryButtonLabel, secondaryButtonTouchTap,
      mainButtonLabel, mainButtonUrl, mainButtonTouchTap, isMainButtonVisible,
      theme
    } = this.props
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
          onTouchTap={secondaryButtonTouchTap}
        />
        {(() => {
          if (isMainButtonVisible) {
            return (
              <MainButtonComponent
                label={mainButtonLabel}
                url={mainButtonUrl}
                onTouchTap={mainButtonTouchTap}
              />
            )
          }
        })()}
      </div>
    )
  }
}


export default CardActionsComponent
