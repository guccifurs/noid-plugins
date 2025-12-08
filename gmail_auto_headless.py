import undetected_chromedriver as uc
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
import random
import time
from datetime import datetime, timedelta
import os

def random_birthday(min_age=18, max_age=70):
    today = datetime.today()
    start_date = datetime(today.year - max_age, 1, 1)
    end_date = datetime(today.year - min_age, 12, 31)
    delta = end_date - start_date
    random_days = random.randint(0, delta.days)
    birth_date = start_date + timedelta(days=random_days)
    return f"{birth_date.day} {birth_date.month} {birth_date.year}"

def timeSleep(min_time): 
    time.sleep(random.randint(min_time, min_time+2))

def take_screenshot(driver, name):
    filename = f"debug_{name}_{int(time.time())}.png"
    driver.save_screenshot(filename)
    print(f"Screenshot saved: {filename}")

def fill_name(driver, wait, your_first_name, your_last_name):
    print("Filling name...")
    try:
        first_name = wait.until(EC.presence_of_element_located((By.NAME, "firstName")))
        first_name.send_keys(your_first_name)
        driver.find_element(By.NAME, "lastName").send_keys(your_last_name)
        take_screenshot(driver, "name_filled")
        driver.find_element(By.CLASS_NAME, "VfPpkd-LgbsSe").click()
        print("Name filled.")
    except Exception as e:
        print(f"Error filling name: {e}")
        take_screenshot(driver, "error_name")
        raise

def fill_birthday_and_gender(driver, wait, your_birthday, your_gender):
    print("Filling birthday and gender...")
    try:
        wait.until(EC.visibility_of_element_located((By.NAME, "day")))
        your_day, your_month, your_year = your_birthday.split()
        
        # Month
        wait.until(EC.element_to_be_clickable((By.ID, "month"))).click()
        wait.until(EC.element_to_be_clickable((By.XPATH, f"//li[@role='option' and @data-value='{int(your_month)}']"))).click()
        
        # Day & Year
        driver.find_element(By.ID, "day").send_keys(your_day)
        driver.find_element(By.ID, "year").send_keys(your_year)
        
        # Gender
        wait.until(EC.element_to_be_clickable((By.ID, "gender"))).click()
        timeSleep(1)
        
        try:
            driver.find_element(By.XPATH, "//li[@role='option' and .//span[text()='Male']]").click()
        except:
            try:
                driver.find_element(By.XPATH, "//li[@role='option' and .//span[text()='Hombre']]").click()
            except:
                options = driver.find_elements(By.XPATH, "//li[@role='option']")
                if len(options) > 1:
                    options[1].click()

        take_screenshot(driver, "birthday_filled")
        driver.find_element(By.CLASS_NAME, "VfPpkd-LgbsSe").click()
        print("Birthday/Gender filled.")
    except Exception as e:
        print(f"Error filling birthday/gender: {e}")
        take_screenshot(driver, "error_birthday")
        raise

def fill_gmailaddress(driver, wait, your_username):
    print("Filling username...")
    try:
        # Check for 'Create your own Gmail address' option
        try:
            options = driver.find_elements(By.CLASS_NAME, "VfPpkd-muHVFf-bMcfAe")
            if options:
                options[-1].click()
                print("Clicked 'Create your own Gmail address' option.")
        except:
            pass

        username_field = wait.until(EC.visibility_of_element_located((By.NAME, "Username")))
        username_field.clear()
        username_field.send_keys(your_username)
        take_screenshot(driver, "username_filled")
        driver.find_element(By.CLASS_NAME, "VfPpkd-LgbsSe").click()
        print("Username filled (clicked Next).")
        
        # Check if we moved to password page
        timeSleep(2)
        if driver.find_elements(By.NAME, "Username"):
            print("Still on username page. Username might be taken.")
            take_screenshot(driver, "username_stuck")
            return False
        return True
        
    except Exception as e:
        print(f"Error filling username: {e}")
        take_screenshot(driver, "error_username")
        raise

def fill_password(driver, wait, your_password):
    print("Filling password...")
    try:
        wait.until(EC.visibility_of_element_located((By.NAME, "Passwd"))).send_keys(your_password)
        driver.find_element(By.NAME, "PasswdAgain").send_keys(your_password)
        take_screenshot(driver, "password_filled")
        driver.find_element(By.CLASS_NAME, "VfPpkd-LgbsSe").click()
        print("Password filled.")
    except Exception as e:
        print(f"Error filling password: {e}")
        take_screenshot(driver, "error_password")
        raise

def main():
    options = uc.ChromeOptions()
    options.add_argument("--headless=new") # Use new headless mode for better stealth
    options.add_argument("--no-sandbox")
    options.add_argument("--disable-dev-shm-usage")
    options.add_argument("--disable-gpu")
    options.add_argument("--window-size=1920,1080")
    options.add_argument("--lang=en-US")

    # uc.Chrome downloads driver automatically
    print("Starting undetected_chromedriver...")
    driver = uc.Chrome(options=options, version_main=143) # Specify version to match installed Chrome

    try:
        first_names = ["James", "John", "Robert", "Michael", "William", "David", "Richard", "Joseph"]
        last_names = ["Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis"]
        
        your_first_name = random.choice(first_names)
        your_last_name = random.choice(last_names)
        
        timestamp = int(time.time())
        random_number = random.randint(1000, 9999)
        your_username = f"{your_first_name.lower()}.{your_last_name.lower()}.{timestamp}"
        your_password = "StrongP@ssw0rd!" + str(random_number)
        your_birthday = random_birthday()
        your_gender = "male"

        print(f"Attempting to create account: {your_username} / {your_password}")

        driver.get("https://accounts.google.com/signup/v2/webcreateaccount?service=mail&continue=https%3A%2F%2Fmail.google.com%2Fmail%2F&flowName=GlifWebSignIn&flowEntry=SignUp&hl=en")
        wait = WebDriverWait(driver, 20)

        take_screenshot(driver, "initial_load")

        fill_name(driver, wait, your_first_name, your_last_name)
        timeSleep(2)
        
        fill_birthday_and_gender(driver, wait, your_birthday, your_gender)
        timeSleep(2)
        
        if not fill_gmailaddress(driver, wait, your_username):
            print("Failed to set username. Exiting.")
            return

        timeSleep(2)
        
        fill_password(driver, wait, your_password)
        timeSleep(5)
        
        take_screenshot(driver, "after_password")

        # Check for various failure states
        page_source = driver.page_source
        if "Verify some info" in page_source or "verify some info" in page_source:
             print("FAILED: Google detected bot/unusual activity ('Verify some info').")
             take_screenshot(driver, "bot_detection")
        elif driver.find_elements(By.ID, "phoneNumberId"):
            print("FAILED: Phone verification required.")
            take_screenshot(driver, "phone_verification")
        elif "INELIGIBLE" in page_source:
             print("FAILED: Account creation failed: Ineligible.")
             take_screenshot(driver, "ineligible")
        else:
            print("Checking for Terms or Skip...")
            
            # Handle skip recovery email if present
            try:
                skip_btns = driver.find_elements(By.XPATH, "//span[text()='Skip']")
                if skip_btns:
                    skip_btns[0].click()
                    print("Clicked Skip on recovery.")
                    timeSleep(2)
                    take_screenshot(driver, "after_recovery_skip")
            except:
                pass
            
            # Agree to terms
            clicked_agree = False
            selectors = [
                "//span[text()='I agree']",
                "//span[contains(text(), 'Agree')]",
                "//button[.//span[contains(text(), 'Agree')]]",
                "//div[@role='button']//span[contains(text(), 'Agree')]"
            ]
            
            for selector in selectors:
                try:
                    btn = wait.until(EC.element_to_be_clickable((By.XPATH, selector)))
                    driver.execute_script("arguments[0].scrollIntoView(true);", btn)
                    timeSleep(1)
                    btn.click()
                    print(f"Clicked I agree using selector: {selector}")
                    clicked_agree = True
                    break
                except:
                    continue
            
            if not clicked_agree:
                # Try JS click
                try:
                    driver.execute_script("""
                        var buttons = document.querySelectorAll('button, div[role="button"]');
                        for (var i = 0; i < buttons.length; i++) {
                            if (buttons[i].textContent.includes('Agree')) {
                                buttons[i].click();
                                return;
                            }
                        }
                    """)
                    print("Attempted JS click on Agree button.")
                    clicked_agree = True
                except:
                    pass

            timeSleep(5)
            take_screenshot(driver, "final_state")
            
            # Final verification
            if "myaccount.google.com" in driver.current_url or "mail.google.com" in driver.current_url:
                print(f"SUCCESS! Account created and logged in:\nEmail: {your_username}@gmail.com\nPassword: {your_password}")
            else:
                print(f"Creation finished but URL is {driver.current_url}. Check screenshots.")
                if "Verify some info" in driver.page_source:
                     print("FAILED: Bot detection triggered at final step.")
                else:
                     print(f"Email: {your_username}@gmail.com\nPassword: {your_password}")

        with open("page_source_final.html", "w") as f:
            f.write(driver.page_source)

    except Exception as e:
        print(f"Error: {e}")
        take_screenshot(driver, "fatal_error")
        with open("page_source_error.html", "w") as f:
            f.write(driver.page_source)
    finally:
        driver.quit()

if __name__ == "__main__":
    main()
