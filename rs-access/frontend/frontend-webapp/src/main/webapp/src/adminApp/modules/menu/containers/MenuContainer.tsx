import I18nProvider from "../../../../common/i18n/I18nProvider"
import MenuComponent from "../components/MenuComponent"

export default class MenuContainer extends React.Component<{}, {}> {

  render (): JSX.Element {

    return (
      <I18nProvider messageDir='adminApp/modules/menu/i18n'>
        <MenuComponent />
      </I18nProvider>
    )
  }

}
