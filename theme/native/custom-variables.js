import * as variables from '../styles/native/core/variables';
import * as custom from '../styles/native/app/custom-variables';
import mergeobjects from '../styles/native/core/helpers/_functions/mergeobjects';

module.exports = { ...mergeobjects(variables, custom) };