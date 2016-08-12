import { connect } from 'react-redux'
import I18nProvider from '../../../../common/i18n/I18nProvider'
import MenuComponent from '../components/MenuComponent'
import * as selectors from "../../../../reducer"

export default class MenuContainer extends React.Component<{}, {}> {

  render(): JSX.Element {

    return (
      <I18nProvider messageDir='adminApp/modules/menu/i18n'>
        <MenuComponent />
      </I18nProvider>
    )
  }

}
