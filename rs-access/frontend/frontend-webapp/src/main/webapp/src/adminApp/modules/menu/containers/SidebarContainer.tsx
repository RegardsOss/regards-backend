import I18nProvider from "../../../../common/i18n/I18nProvider"
import SidebarComponent from "../components/SidebarComponent"

class SidebarContainer extends React.Component<{}, {}> {

  render (): JSX.Element {

    return (
      <I18nProvider messageDir='adminApp/modules/menu/i18n'>
        <SidebarComponent />
      </I18nProvider>
    )
  }

}

export default SidebarContainer
