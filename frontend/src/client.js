import * as sapper from '@sapper/app';
import Controller from "./api/controller.ts";

Controller.setup();

sapper.start({
    target: document.querySelector('#sapper')
});
