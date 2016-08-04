import I18nProvider from '../../../../common/i18n/I18nProvider'
import MenuComponent from '../components/MenuComponent'

class MenuContainer extends React.Component<{}, any> {

  render(): any {

    return (
      <I18nProvider messageDir='adminApp/modules/menu/i18n'>
        <MenuComponent />
      </I18nProvider>
    )
  }

}

export default MenuContainer
