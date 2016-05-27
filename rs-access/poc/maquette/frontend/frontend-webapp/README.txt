##################
# Source codes
##################

webapp/
  adminApp/ : Contains the admin web application
  common/ : Contains the common modules.
      moduleManager/ :
          Contains the root RegardsView which allow to manage views
          access rights from REST dependencies.
      pluginManager/ :
          Contains the PluginController which allow to load plugins if any.
          Contains the PluginView which allow to display a given loaded
          plugins.
      store/ :
          Contains the Redux Store. Each module must register his reducers
          to the store.
  plugins/ : Contains all embeded plugins.
  portalApp/ : Contains the portal web applicaion
      pluginModule/ :
          Contains the views to display plugins in the user app.
      testModule/ :
          Contains a view example for access denied to REST resource. This view
          shall not be display.
  userApp/ : Contains the user web application
  json/ : mocks for not emplemented backend


################
#  RegardsView :
################

    All views in the regards application should extends RegardsView.

    Example :

    class ExempleView extends RegardsView {

      // Define all REST dependencies of the view
      getDependencies(){
        return null // For no dependencies
        or
        return {
          "GET" : [...],
          "POST" : [...],
          "PUT": [...],
          "DELETE" [...]
        }
      }

      // Define the render method of the view
      renderView(){
         ....
      }
    }
################
# plugins
################

   All plugins in plugins/ directory have to be React components.
