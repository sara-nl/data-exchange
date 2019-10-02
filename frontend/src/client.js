import * as sapper from '@sapper/app';
import Controller from "./api/controller.ts";
import { token } from "./stores/token.ts";
import 'bootstrap';

token.useLocalStorage();
Controller.setup(sapper);

sapper.start({
    target: document.querySelector('#sapper')
});
