import {useEffect, useState} from 'react';

export default function useLocalStorage<T>(
    key: string,
    defaultValue: T
): [T, (value: T) => void] {
    const [value, setValue] = useState<T>(() => {
        try {
            const storedValue = localStorage.getItem(key);
            if (storedValue) {
                return JSON.parse(storedValue);
            }
        } catch (error) {
            console.warn(`Error parsing localStorage key "${key}":`, error);
        }
        return defaultValue;
    });

    useEffect(() => {
        if (value === undefined) return;
        try {
            localStorage.setItem(key, JSON.stringify(value));
        } catch (error) {
            console.warn(`Error saving to localStorage key "${key}":`, error);
        }
    }, [value, key]);

    return [value, setValue];
}
