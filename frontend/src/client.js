import "bootstrap";
import * as sapper from '@sapper/app';
import { token, mode, email } from "./stores";
import Controller from "./api/controller";

token.useLocalStorage();
email.useLocalStorage();
mode.useLocalStorage();
Controller.setup(sapper);

sapper.start({
    target: document.querySelector('#sapper')
});
