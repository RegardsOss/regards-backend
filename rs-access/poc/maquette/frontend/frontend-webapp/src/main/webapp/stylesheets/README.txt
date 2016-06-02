
1. loader sass (sass-loader)

   compile les fichiers sass en css

2. Decoupe en module (css?modules)

   Pour chaque composant react, un fichier css peut etre associé.
   Les classes contenues dans ce fichier sont alors spécifiques au composant react.
   Cela signi que deux composants peuvent avoir les même noms de classes.
   Ce loader s'occupe de rendre unique les noms des classes css par composant REACT.

   Exemple :

   monComposant.js :
   import styles from 'monComposant.sass';
   class monComposant extends React.Component {
    render(){
      return <span styleName="label">test</span>;
    }
   }
   export default CSSModules(monComposant, styles);

   config.sass :
   $color=Black

   monComposant.sass
   @import(config.sass)
   .label
      color: $color

3. Compiler les css dans un fichier unique

  Utilisation du plugin de webpack ExtractTextPlugin.
  Ce plugin permet d'ajouter tous les css dans un seul et unique fichier.
  Pour ce faire, il faut un loader de fichier css :

  {test: /\.css$/
    loader: ExtractTextPlugin.extract('style-loader');
  }

  et définir le plugin dans webpack en indiquant le nom et l'emplacement du fichier contenant tous les css.

  plugins: [
    new ExtractTextPlugin('styles.css')
  ],

3. Config webpack globale

  const sassLoaders = [
    'css?modules&importLoaders=1&localIdentName=[path]___[name]__[local]___[hash:base64:5]',
    'sass-loader?indentedSyntax=sass&includePaths[]=' + path.resolve(__dirname, 'steelsheets')
  ]
  module: {
    loaders : [
      ...,
      {test: /\.sass$/
      loader: ExtractTextPlugin.extract('style-loader', sassLoaders.join('!'))
      }
    ]
  },
  plugins: [
    new ExtractTextPlugin('styles.css')
  ]


4. templates
