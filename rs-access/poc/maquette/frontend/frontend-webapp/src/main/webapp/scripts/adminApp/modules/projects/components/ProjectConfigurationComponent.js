import React, {Component, PropTypes } from 'react'
import {reduxForm} from 'redux-form';
import icons from 'stylesheets/foundation-icons/foundation-icons.scss'

class ProjectConfigurationComponent extends Component {
  render() {
    const {fields: {firstName, lastName, email}, handleSubmit} = this.props;
    return (
      <form onSubmit={handleSubmit}>
        <div>
          <label>First Name</label>
          <input type="text" placeholder="First Name" {...firstName}/>
        </div>
        <div>
          <label>Last Name</label>
          <input type="text" placeholder="Last Name" {...lastName}/>
        </div>
        <div>
          <label>Email</label>
          <input type="email" placeholder="Email" {...email}/>
        </div>
        <button type="submit">Submit</button>
      </form>
    );
  }
}

ProjectConfigurationComponent = reduxForm({ // <----- THIS IS THE IMPORTANT PART!
  form: 'ProjectConfigurationForm',                           // a unique name for this form
  fields: ['firstName', 'lastName', 'email'] // all the fields in your form
})(ProjectConfigurationComponent);

export default ProjectConfigurationComponent
