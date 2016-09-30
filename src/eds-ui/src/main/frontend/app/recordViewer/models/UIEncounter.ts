module app.models {
    'use strict';

    export class UIEncounter {
        status: string;
        performedBy: UIPractitioner;
        enteredBy: UIPractitioner;
        reason: UICode[];
        period: UIPeriod;
    }
}