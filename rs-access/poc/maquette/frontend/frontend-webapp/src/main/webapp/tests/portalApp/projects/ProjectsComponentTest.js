import expect from 'expect'
import React from 'react'
import { Link } from 'react-router';
import { mount, shallow } from 'enzyme';
// Import unconnected version of ProjectsComponent. by using bracets {} around component.
// To get the react-redux connect component use "import ProjectsComponent" instead of "import { ProjectsComponent }"
import { ProjectsComponent } from '../../../scripts/portalApp/projects/ProjectsComponent';
import ProjectComponent from '../../../scripts/portalApp/projects/ProjectComponent';

// Test a component rendering

describe('Testing projects components', () => {
  it('Should render correctly the loading projects message', () => {
    const dispatch = () => { };
    const projectsStyles = {
      link: 'link'
    };
    let props = {
      projects: {
        isFetching: true
      },
      styles: projectsStyles,
      dispatch :dispatch
    };
    const wrapper = shallow(<ProjectsComponent {...props}/>);
    expect(wrapper.equals(<div>Loading projects ... </div>)).toEqual(true);
  });

  it('Should render correctly the projects list', () => {
    const dispatch = () => { };
    const projectsStyles = {
      link: 'link'
    };
    let props = {
      projects: {
        isFetching: false,
        items: [{name: 'cdpp'},{name: 'ssalto'}]
      },
      styles: projectsStyles,
      dispatch :dispatch
    };

    const result = (
      <div>
        <p>Available projects on REGARDS instance :</p>
        <ul>
            <ProjectComponent key="cdpp" project={{name: 'cdpp'}} styles={projectsStyles}/>
            <ProjectComponent key= "ssalto" project={{name: 'ssalto'}} styles={projectsStyles}/>
        </ul>
      </div>
    );
    const wrapper = shallow(<ProjectsComponent {...props}/>);
    console.log(wrapper.debug());
    expect(wrapper.contains(result)).toEqual(true);
  });

  it('Should render correctly a project link', () => {
    const projectsStyles = {
      link: 'link'
    };
    let props = {
      styles: projectsStyles,
      project: {name: 'cdpp'}
    };

    const result = (
      <li className="link">
        cdpp
        &nbsp;-&nbsp;
        <Link to="/user/cdpp" onlyActiveOnIndex={false} style={{}}>ihm user</Link>
        &nbsp;/&nbsp;
        <Link to="/admin/cdpp" onlyActiveOnIndex={false} style={{}}>ihm admin</Link>
      </li>
    );
    const wrapper = shallow(<ProjectComponent {...props}/>);
    console.log(wrapper.debug());
    expect(wrapper.contains(result)).toEqual(true);
  });

});
