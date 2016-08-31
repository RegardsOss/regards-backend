import I18nProvider from "../../../../common/i18n/I18nProvider"
import SidebarComponent from "../components/SidebarComponent"
import ThemeInjector from "../../../../common/theme/ThemeInjector"

class SidebarContainer extends React.Component<{}, {}> {

  render (): JSX.Element {

    return (
      <I18nProvider messageDir='adminApp/modules/menu/i18n'>
        <ThemeInjector>
          <SidebarComponent theme={null} />
        </ThemeInjector>
      </I18nProvider>
    )
  }

}

export default SidebarContainer
