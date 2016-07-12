import * as React from 'react'
import { Component, PropTypes } from 'react'
import {reduxForm} from 'redux-form';
var icons = require('stylesheets/foundation-icons/foundation-icons.scss')

interface ProjectconfigurationTypes {
  fields?: Array<any>
  handleSubmit: () => void,
  onCancelClick: () => void,
  onSaveClick?: ()=> void,
  submitting: boolean,
  show: boolean,
  styles: any
}

class ProjectConfigurationComponent extends Component<ProjectconfigurationTypes, any> {
  render() {
    const { show, onSaveClick, onCancelClick }:any = this.props;
    const {  fields: {projectName}, handleSubmit,  submitting,styles }:any = this.props

    if(show)
      return (
        <form onSubmit={handleSubmit}>
          <div>
            <input type="text" placeholder="Project Name" {...projectName}/>
          </div>
          <div>
            <button className={styles['button'] + ' ' + styles['success']} disabled={submitting}>
              <i className={icons['fi-save']}></i>
              {submitting ? 'Submitting...' : ''} Save
            </button>
            <button type="button" className={styles['button'] + ' ' + styles['alert']} disabled={submitting} onClick={onCancelClick}>
              <i className={icons['fi-prohibited']}></i>
              Cancel
            </button>
          </div>
        </form>
      );
    else
      return null
  }
}

// const validate = values => {
//   const errors = {}
//   if (!values.username) {
//     errors.username = 'Required'
//   } else if (values.username.length > 15) {
//     errors.username = 'Must be 15 characters or less'
//   }
//   return errors
// }

export default reduxForm({ // <----- THIS IS THE IMPORTANT PART!
  form: 'ProjectConfigurationForm',                           // a unique name for this form
  fields: ['projectName'] // all the fields in your form
  // validate
})(ProjectConfigurationComponent);
