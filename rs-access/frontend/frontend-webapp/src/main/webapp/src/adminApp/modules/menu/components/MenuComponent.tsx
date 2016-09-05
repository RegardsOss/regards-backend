import I18nProvider from "../../../../common/i18n/I18nProvider"
import { Toolbar, ToolbarGroup, ToolbarSeparator, ToolbarTitle } from "material-ui/Toolbar"
import SelectTheme from "../../../../common/theme/containers/SelectTheme"
import SelectLocaleContainer from "../../../../common/i18n/containers/SelectLocaleContainer"
import { ThemeContextInterface, ThemeContextType } from "../../../../common/theme/ThemeContainerInterface"

class MenuComponent extends React.Component<{}, {}> {

  static contextTypes: Object = ThemeContextType
  context: ThemeContextInterface

  render (): JSX.Element {

    const style = {
      headContainer: {
        classes: this.context.muiTheme.adminApp.layout.headContainer.classes.join(' '),
        styles: Object.assign({}, this.context.muiTheme.adminApp.layout.headContainer.styles, {fontFamily: this.context.muiTheme.fontFamily}),
      },
      title: this.context.muiTheme.toolbarTitle,
    }
    return (
      <I18nProvider messageDir='adminApp/modules/menu/i18n'>

        <Toolbar className={style.headContainer.classes} style={style.headContainer.styles}>
          <ToolbarGroup firstChild={true}>
            <ToolbarTitle text="REGARDS admin dashboard" style={style.title}/>
          </ToolbarGroup>

          <ToolbarGroup>
            <SelectLocaleContainer locales={['en','fr']}/>

            <ToolbarSeparator />

            <SelectTheme />
          </ToolbarGroup>
        </Toolbar>
      </I18nProvider>
    )
  }

}
export default MenuComponent
