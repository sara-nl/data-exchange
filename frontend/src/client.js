import 'bootstrap/dist/css/bootstrap.min.css'
import 'bootstrap/dist/js/bootstrap.min.js'
import 'highlight.js/styles/github.css'

import * as sapper from '@sapper/app'
import { token, mode, email } from './stores'
import Controller from './api/controller'

token.useLocalStorage()
email.useLocalStorage()
mode.useLocalStorage()
Controller.setup(sapper)

sapper.start({
  target: document.querySelector('#sapper'),
})
