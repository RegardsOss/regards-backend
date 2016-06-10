import React from 'react';

class SelectThemeComponent extends React.Component {
  constructor(){
    super();
    this.onChange = this.onChange.bind(this);
  }

  onChange(e){
    console.log("SEB",e.target.value);
    this.props.onThemeChange(e.target.value);
  }

  componentWillMount(){
    this.setState({
      selectedValue: this.props.curentTheme
    });
  }

  render(){
    const { styles, themes, onThemeChange } = this.props;

    return (
      <div className={styles["select-theme"]}>
        <span> Select your theme : </span>
        <select
          value={this.props.curentTheme}
          onChange={this.onChange}>
            {themes.map( (theme) => {
                return <option key={theme} value={theme}>{theme}</option>;
            })}
        </select>
      </div>
    );
  }

}

SelectThemeComponent.propTypes = {
  styles: React.PropTypes.object.isRequired,
  themes: React.PropTypes.array.isRequired,
  curentTheme: React.PropTypes.string.isRequired,
  onThemeChange: React.PropTypes.func.isRequired
}

export default SelectThemeComponent;
