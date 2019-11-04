import "bootstrap";
import * as sapper from '@sapper/app';
import { token, mode, email } from "./stores.ts";
import Controller from "./api/controller.ts";

token.useLocalStorage();
email.useLocalStorage();
mode.useLocalStorage();
Controller.setup(sapper);

sapper.start({
    target: document.querySelector('#sapper')
});
