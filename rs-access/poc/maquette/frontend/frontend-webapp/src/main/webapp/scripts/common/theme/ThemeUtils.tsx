

/**
* Supply the css styles of a given css style file from a given theme.
* Exemple : getThemeStyles( 'myFirstTheme' , 'userApp/login')
*   This exemple supplies the css classes from the userApp/login.css file for the theme 'myFirstTheme'
*/
const getThemeStyles = (theme:string, style:any) => {
  if (theme && theme !== "" && style && style !== ""){
    return require('stylesheets/themes/'+theme+'/'+style)
  } else if (style && style !== ""){
    return require('stylesheets/themes/default/'+style)
  } else {
    return {}
  }
}

export { getThemeStyles }
