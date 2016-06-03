

const getThemeStyles = (theme, style) => {
  if (theme && theme !== "" && style && style !== ""){
    return require('stylesheets/themes/'+theme+'/'+style);
  } else if (style && style !== ""){
    return require('stylesheets/themes/default/'+style);
  } else {
    return {}
  }
}

export { getThemeStyles }
