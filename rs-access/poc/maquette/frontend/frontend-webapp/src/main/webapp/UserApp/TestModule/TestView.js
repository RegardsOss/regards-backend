import RegardsView from 'RegardsView';
class TestView extends RegardsView {

  getDependencies(){
    return {
      "GET" : ["dependence"]
    }
  }

  renderView(){
    return (<div>This view shall not be displayed ! </div>);
  }
}

export default TestView
